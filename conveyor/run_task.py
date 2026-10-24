"""Дирижёр: задание → проверка main → ветка task/<id> → (исполнитель ⇄ verify) до max_tries → коммит или стоп.

    uv run conveyor/run_task.py tasks/001-x.md [--tries 3] [--executor-model opus] [--judge-model sonnet]

Сам код не пишет. Не пушит и не открывает PR. Метрики (время, токены, стоимость) — reports/<id>/metrics.json.
Коды выхода: 0 — зелёный и закоммичен, 1 — красный после всех попыток, 2 — нужен человек.
Упрощённая копия run_task.py из project-keys-v2: без ступеней доверия, правил PENDING, Godot.
"""
import argparse
import json
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from checks import (CONVEYOR, ROOT, changed_files, claude_json, git, load_config, load_role,  # noqa: E402
                    load_task, matches_any, reports_dir)

GIT_FORBIDDEN = ["commit", "push", "checkout", "switch", "reset", "stash", "branch", "rebase", "merge", "restore"]
VERIFY = ["uv", "run", "--no-project", "conveyor/verify.py"]


def sh_verify(args: list[str]) -> bool:
    return subprocess.run([*VERIFY, *args], cwd=ROOT).returncode == 0


def feedback_from(out_dir: Path, try_no: int) -> str:
    path = out_dir / f"verify_try{try_no}.json"
    if not path.exists():
        return ""
    data = json.loads(path.read_text(encoding="utf-8"))
    parts = [f"\n## Попытка {try_no} не прошла проверку. Исправь:"]
    for r in data["results"]:
        if r["status"] == "FAIL":
            parts.append(f"\n### {r['name']}: {r['summary']}\n```\n{r['details'][-4000:]}\n```")
    return "\n".join(parts)


def run_executor(task: dict, model: str, feedback: str, timeout_min: int, log: Path) -> dict:
    prompt = (CONVEYOR / "prompts" / "executor.md").read_text(encoding="utf-8").format(
        role=task["role"], role_path=f"conveyor/roles/{task['role']}.md", task_path=task["path"],
        report_dir=f"reports/{task['id']}", feedback=feedback)
    denied = [f"Edit({p})" for p in load_config()["protected"]] + [f"Bash(git {c}:*)" for c in GIT_FORBIDDEN]
    print(f"\n=== Исполнитель ({model}) работает... ===", flush=True)
    code, text, metrics = claude_json(
        ["--model", model, "--permission-mode", "acceptEdits",
         "--allowedTools", "Read", "Edit", "Write", "Glob", "Grep", "Bash", "--disallowedTools", *denied],
        prompt, timeout_min * 60)
    log.write_text(text, encoding="utf-8")
    print(text[-1500:], flush=True)
    return {"exit": code, **metrics}


def judge_metrics(out_dir: Path, try_no: int) -> dict:
    path = out_dir / f"verify_try{try_no}.json"
    if not path.exists():
        return {}
    for r in json.loads(path.read_text(encoding="utf-8"))["results"]:
        if r["name"] == "judge":
            return {"status": r["status"], **(r.get("data") or {}).get("metrics", {})}
    return {}


def main() -> int:
    sys.stdout.reconfigure(encoding="utf-8")
    p = argparse.ArgumentParser()
    p.add_argument("task")
    p.add_argument("--tries", type=int)
    p.add_argument("--executor-model", choices=["opus", "sonnet"])
    p.add_argument("--judge-model", choices=["opus", "sonnet"])
    a = p.parse_args()

    config = load_config()
    task = load_task(a.task)
    role = load_role(task["role"])
    tries = a.tries or int(task.get("max_tries") or config["max_tries"])
    ex_model = a.executor_model or task.get("executor_model") or config["executor_model"]
    j_model = a.judge_model or task.get("judge_model") or config["judge_model"]
    base, branch = config["base_branch"], f"task/{Path(task['path']).stem}"
    out_dir = reports_dir(task)
    print(f"Задание {task['id']} «{task['title']}», роль {task['role']}, ветка {branch}, попыток {tries}")

    if git("status", "--porcelain", "--untracked-files=no").strip():
        print("СТОП: есть незакоммиченные изменения в отслеживаемых файлах.")
        return 2
    started = time.monotonic()
    git("checkout", base)
    print(f"\n=== Базовая проверка {base} (запоминаем, какие тесты уже красные) ===")
    if not sh_verify(["--baseline"]):
        print(f"ВНИМАНИЕ: на {base} есть красные тесты. Задание не должно добавить новых.")
    git("checkout", branch) if git("branch", "--list", branch).strip() else git("checkout", "-b", branch)
    (out_dir / "questions.md").unlink(missing_ok=True)

    metrics = {"task": task["id"], "executor_model": ex_model, "judge_model": j_model, "tries": []}
    result, code = "red", 1
    for try_no in range(1, tries + 1):
        feedback = feedback_from(out_dir, try_no - 1) if try_no > 1 else ""
        ex = run_executor(task, ex_model, feedback, config["executor_timeout_min"], out_dir / f"executor_try{try_no}.md")
        q = out_dir / "questions.md"
        if q.exists() and q.read_text(encoding="utf-8").strip():
            metrics["tries"].append({"try": try_no, "executor": ex, "verdict": "questions"})
            print(f"\nНУЖЕН ЧЕЛОВЕК: вопросы исполнителя — reports/{task['id']}/questions.md")
            result, code = "questions", 2
            break
        print(f"\n=== Проверка, попытка {try_no}/{tries} ===")
        green = sh_verify(["--task", task["path"], "--judge-model", j_model, "--try", str(try_no)])
        metrics["tries"].append({"try": try_no, "executor": ex, "judge": judge_metrics(out_dir, try_no),
                                 "verdict": "green" if green else "red"})
        if green:
            mine = [f for f in changed_files(base) if matches_any(f, role["allowed"])]
            if mine:
                git("add", "--", *mine, f"reports/{task['id']}")
                git("commit", "-m", f"Задание {task['id']}: {task['title']}\n\nРоль: {task['role']}. "
                    f"Попыток: {try_no}. Отчёт: reports/{task['id']}/report.md\n\n"
                    "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>")
            print(f"\nЗЕЛЁНЫЙ с попытки {try_no}. Ветка {branch}. Отчёт: reports/{task['id']}/report.md")
            result, code = "green", 0
            break
    else:
        print(f"\nКРАСНЫЙ после {tries} попыток. Изменения в {branch} без коммита.")

    costs = [t.get(k, {}).get("cost_usd") or 0 for t in metrics["tries"] for k in ("executor", "judge")]
    metrics.update(result=result, wall_s=round(time.monotonic() - started), total_cost_usd=round(sum(costs), 3))
    (out_dir / "metrics.json").write_text(json.dumps(metrics, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Время {metrics['wall_s']} с, стоимость ${metrics['total_cost_usd']}")
    return code


if __name__ == "__main__":
    sys.exit(main())
