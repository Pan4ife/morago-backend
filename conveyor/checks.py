"""Проверки конвейера morago: границы роли, сборка+тесты Maven, LLM-судья. Плюс общие помощники (git, задание, роль).

Каждая проверка — функция check_<имя>(ctx) -> Result. Упрощённая копия checks/ из project-keys-v2.
"""
from __future__ import annotations

import json
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CONVEYOR = ROOT / "conveyor"
PASS, FAIL, SKIP = "PASS", "FAIL", "SKIP"


@dataclass
class Result:
    name: str
    status: str
    summary: str
    details: str = ""
    seconds: float = 0.0
    data: dict = field(default_factory=dict)


@dataclass
class Context:
    config: dict
    task: dict | None
    role: dict | None
    judge_model: str = ""
    previous: list[Result] = field(default_factory=list)


# ---------- общее ----------

def load_config() -> dict:
    return json.loads((CONVEYOR / "config.json").read_text(encoding="utf-8"))


def run_cmd(args: list[str], timeout: float, stdin: str | None = None) -> tuple[int, str]:
    try:
        p = subprocess.run(args, cwd=ROOT, input=stdin, capture_output=True, text=True,
                           encoding="utf-8", errors="replace", timeout=timeout)
        return p.returncode, (p.stdout or "") + (p.stderr or "")
    except subprocess.TimeoutExpired:
        return 124, f"[таймаут {timeout:.0f} с]"
    except FileNotFoundError as e:
        return 127, f"[команда не найдена: {e}]"


def git(*args: str) -> str:
    p = subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True,
                       encoding="utf-8", errors="replace", timeout=60)
    if p.returncode != 0:
        raise RuntimeError(f"git {' '.join(args)}: {p.stdout}{p.stderr}")
    return p.stdout


def current_branch() -> str:
    return git("branch", "--show-current").strip()


def changed_files(base: str, only_added: bool = False) -> list[str]:
    flt = ["--diff-filter=A"] if only_added else []
    files: set[str] = set()
    if current_branch() != base:
        files.update(git("diff", "--name-only", *flt, f"{base}...HEAD").split())
    files.update(git("diff", "--name-only", *flt, "HEAD").split())
    files.update(git("ls-files", "--others", "--exclude-standard").splitlines())
    return sorted(f.strip() for f in files if f.strip())


def diff_text(base: str, limit: int = 60000) -> str:
    parts = [git("diff", base)]
    for f in git("ls-files", "--others", "--exclude-standard").splitlines():
        p = ROOT / f
        if p.is_file() and p.stat().st_size < 50000:
            parts.append(f"\n=== НОВЫЙ ФАЙЛ {f} ===\n" + p.read_text(encoding="utf-8", errors="replace"))
    text = "\n".join(parts)
    return text if len(text) <= limit else text[:limit] + "\n[... diff обрезан ...]"


def glob_to_regex(pattern: str) -> re.Pattern:
    out = re.escape(pattern).replace(r"\*\*", ".*").replace(r"\*", "[^/]*")
    return re.compile(out + "$")


def matches_any(path: str, patterns: list[str]) -> bool:
    return any(glob_to_regex(p).match(path) for p in patterns)


def parse_front_matter(path: Path) -> tuple[dict, str]:
    """Шапка между --- : `ключ: значение` и списки `  - элемент`."""
    text = path.read_text(encoding="utf-8")
    m = re.match(r"^---\s*\n(.*?)\n---\s*\n(.*)$", text, re.S)
    if not m:
        return {}, text
    meta: dict = {}
    key = None
    for line in m.group(1).splitlines():
        item = re.match(r"^\s+-\s*(.+)$", line)
        if item and key:
            if not isinstance(meta.get(key), list):
                meta[key] = []
            meta[key].append(item.group(1).strip())
            continue
        kv = re.match(r"^([\w-]+):\s*(.*)$", line)
        if kv:
            key = kv.group(1)
            meta[key] = kv.group(2).strip()
    return meta, m.group(2)


def split_list(value) -> list[str]:
    if isinstance(value, list):
        return value
    return [v.strip() for v in str(value or "").split(",") if v.strip()]


def load_task(path: str) -> dict:
    p = (ROOT / path).resolve()
    meta, body = parse_front_matter(p)
    for required in ("id", "title", "role"):
        if not meta.get(required):
            raise ValueError(f"в задании {path} нет поля `{required}`")
    meta["tests"] = split_list(meta.get("tests"))
    meta["path"] = p.relative_to(ROOT).as_posix()
    meta["body"], meta["text"] = body, p.read_text(encoding="utf-8")
    return meta


def load_role(name: str) -> dict:
    p = CONVEYOR / "roles" / f"{name}.md"
    if not p.exists():
        raise ValueError(f"роли `{name}` нет: ожидался conveyor/roles/{name}.md")
    meta, _ = parse_front_matter(p)
    meta["allowed"] = split_list(meta.get("allowed"))
    meta["text"] = p.read_text(encoding="utf-8")
    return meta


def reports_dir(task: dict | None) -> Path:
    d = ROOT / "reports" / (str(task["id"]) if task else "baseline")
    d.mkdir(parents=True, exist_ok=True)
    return d


def tail(text: str, lines: int = 60) -> str:
    return "\n".join(text.rstrip().splitlines()[-lines:])


def claude_json(args: list[str], prompt: str, timeout: float) -> tuple[int, str, dict]:
    """claude -p --output-format json: (код, текст ответа, метрики — стоимость, токены, время, ходы)."""
    exe = shutil.which("claude") or "claude"
    code, out = run_cmd([exe, "-p", "--output-format", "json", *args], timeout, stdin=prompt)
    try:
        data = json.loads(out[out.index("{"):])
    except (ValueError, json.JSONDecodeError):
        return code, out, {}
    usage = data.get("usage") or {}
    metrics = {
        "cost_usd": data.get("total_cost_usd"),
        "duration_s": round((data.get("duration_ms") or 0) / 1000, 1),
        "turns": data.get("num_turns"),
        "input_tokens": usage.get("input_tokens"),
        "cache_read_tokens": usage.get("cache_read_input_tokens"),
        "cache_write_tokens": usage.get("cache_creation_input_tokens"),
        "output_tokens": usage.get("output_tokens"),
    }
    return code, data.get("result") or "", metrics


# ---------- проверки ----------

def check_scope(ctx: Context) -> Result:
    """Изменены только папки своей роли, защищённые файлы не тронуты."""
    name = "scope"
    if not ctx.task:
        return Result(name, SKIP, "базовая проверка — границы не проверяются")
    cfg = ctx.config
    files = [f for f in changed_files(cfg["base_branch"]) if not matches_any(f, cfg["ignored_for_scope"])]
    protected = [f for f in files if matches_any(f, cfg["protected"])]
    foreign = [f for f in files if f not in protected and not matches_any(f, ctx.role["allowed"])]
    lines = []
    if protected:
        lines += ["Тронуты защищённые файлы (их меняет только человек):"] + [f"  - {f}" for f in protected]
    if foreign:
        lines += [f"Файлы вне папок роли `{ctx.task['role']}` ({', '.join(ctx.role['allowed'])}):"]
        lines += [f"  - {f}" for f in foreign]
    if lines:
        return Result(name, FAIL, f"нарушены границы: защищённых {len(protected)}, чужих {len(foreign)}",
                      "\n".join(lines))
    return Result(name, PASS, f"изменено файлов: {len(files)}, все в папках роли", "\n".join(files))


def run_maven(timeout_min: int) -> tuple[int, str, dict[str, str]]:
    """./mvnw test, падения тестов не валят сборку. Возвращает (код, вывод, {Класс.тест: PASS|FAIL|SKIP})."""
    mvnw = str(ROOT / ("mvnw.cmd" if sys.platform == "win32" else "mvnw"))
    reports = ROOT / "target" / "surefire-reports"
    if reports.exists():
        shutil.rmtree(reports)
    code, out = run_cmd([mvnw, "-B", "-q", "test", "-Dmaven.test.failure.ignore=true"], timeout_min * 60)
    tests: dict[str, str] = {}
    for xml in reports.glob("TEST-*.xml") if reports.exists() else []:
        for case in ET.parse(xml).getroot().iter("testcase"):
            key = f"{case.get('classname', '').split('.')[-1]}.{case.get('name')}"
            kids = {c.tag for c in case}
            tests[key] = FAIL if kids & {"failure", "error"} else SKIP if "skipped" in kids else PASS
    return code, out, tests


def baseline_file() -> Path:
    return reports_dir(None) / "tests.json"


def check_tests(ctx: Context) -> Result:
    """Сборка проходит; число тестов не уменьшилось; нет новых красных; тесты из `tests:` задания зелёные."""
    name = "build_tests"
    code, out, tests = run_maven(ctx.config["mvn_timeout_min"])
    failed = sorted(t for t, s in tests.items() if s == FAIL)
    if code != 0 or not tests:
        return Result(name, FAIL, f"сборка не прошла (код {code}, тестов найдено {len(tests)})", tail(out, 80))
    if not ctx.task:  # базовая проверка: запомнить, что зелёное и что красное на main
        commit = git("rev-parse", "HEAD").strip()
        baseline_file().write_text(json.dumps({"commit": commit, "tests": tests}, ensure_ascii=False, indent=2),
                                   encoding="utf-8")
        status = PASS if not failed else FAIL
        return Result(name, status, f"тестов {len(tests)}, красных {len(failed)}", "\n".join(failed),
                      data={"tests": tests})
    base = json.loads(baseline_file().read_text(encoding="utf-8"))["tests"] if baseline_file().exists() else {}
    problems = []
    if len(tests) < len(base):
        gone = sorted(set(base) - set(tests))
        problems.append(f"тестов стало меньше: было {len(base)}, стало {len(tests)}. Пропали: {', '.join(gone)}")
    new_red = [t for t in failed if base.get(t) != FAIL]
    if new_red:
        problems.append("новые красные тесты (на main были зелёными или их не было): " + ", ".join(new_red))
    for pattern in ctx.task["tests"]:
        hit = {t: s for t, s in tests.items() if t == pattern or t.startswith(pattern + ".")}
        if not hit:
            problems.append(f"обязательный тест `{pattern}` (tests: в задании) не найден")
        bad = [t for t, s in hit.items() if s != PASS]
        if bad:
            problems.append(f"обязательные тесты не зелёные: {', '.join(bad)}")
    details = "\n".join(problems + ["", "Красные сейчас: " + (", ".join(failed) or "нет"), "", tail(out, 40)])
    if problems:
        return Result(name, FAIL, f"тестов {len(tests)}, проблем {len(problems)}", details, data={"tests": tests})
    known = len(failed)
    return Result(name, PASS, f"тестов {len(tests)} (было {len(base)}), красных {known} — все были красными на main",
                  details, data={"tests": tests})


JSON_BLOCK = re.compile(r"```json\s*(\{.*?\})\s*```", re.S)


def check_judge(ctx: Context) -> Result:
    """LLM-судья читает задание, роль, итоги проверок и diff; выносит JSON-вердикт."""
    name = "judge"
    if not ctx.task:
        return Result(name, SKIP, "базовая проверка — судья не нужен")
    checks = "\n".join(f"- {r.name}: {r.status} — {r.summary}" for r in ctx.previous)
    prompt = "\n\n".join([
        (CONVEYOR / "prompts" / "judge.md").read_text(encoding="utf-8"),
        "# Задание\n" + ctx.task["text"],
        "# Паспорт роли\n" + ctx.role["text"],
        "# Итоги автоматических проверок\n" + checks,
        f"# Diff относительно {ctx.config['base_branch']}\n```diff\n{diff_text(ctx.config['base_branch'])}\n```",
    ])
    code, out, metrics = claude_json(
        ["--model", ctx.judge_model, "--allowedTools", "Read", "Grep", "Glob",
         "--disallowedTools", "Edit", "Write", "Bash"], prompt, ctx.config["judge_timeout_min"] * 60)
    (reports_dir(ctx.task) / "judge_raw.md").write_text(out, encoding="utf-8")
    blocks = JSON_BLOCK.findall(out)
    if code != 0 or not blocks:
        return Result(name, FAIL, f"судья не дал вердикт (код {code})", tail(out, 40), data={"metrics": metrics})
    try:
        verdict = json.loads(blocks[-1])
    except json.JSONDecodeError as e:
        return Result(name, FAIL, f"вердикт судьи — не JSON: {e}", blocks[-1], data={"metrics": metrics})
    lines = [f"[{c.get('status')}] {c.get('criterion')} — {c.get('evidence')}" for c in verdict.get("criteria", [])]
    lines += [f"ПРОБЛЕМА: {i}" for i in verdict.get("issues", [])]
    lines += [f"заметка: {n}" for n in verdict.get("notes", [])]
    ok = verdict.get("verdict") == "PASS" and not verdict.get("issues")
    verdict["metrics"] = metrics
    return Result(name, PASS if ok else FAIL, f"судья {ctx.judge_model}: {'принято' if ok else 'отклонено'}",
                  "\n".join(lines), data=verdict)
