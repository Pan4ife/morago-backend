"""Проверяющий: границы роли → сборка и тесты → судья. Сам ничего не исправляет. Пишет отчёт reports/<id>/report.md.

    uv run conveyor/verify.py --task tasks/001-x.md            полная проверка (с судьёй)
    uv run conveyor/verify.py --task tasks/001-x.md --no-judge  без судьи (так проверяет себя исполнитель)
    uv run conveyor/verify.py --baseline                        проверка main: запоминает зелёные/красные тесты
Код выхода 0 — зелёный, 1 — красный.
"""
import argparse
import json
import sys
import time
from dataclasses import asdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from checks import (FAIL, PASS, SKIP, ROOT, Context, Result, changed_files, check_judge, check_scope,  # noqa: E402
                    check_tests, current_branch, load_config, load_role, load_task, reports_dir)

MARK = {PASS: "[ OK ]", FAIL: "[FAIL]", SKIP: "[skip]"}


def run_check(fn, ctx: Context) -> Result:
    start = time.monotonic()
    try:
        result = fn(ctx)
    except Exception as e:  # упавшая проверка — красное, а не тихий пропуск
        result = Result(fn.__name__.removeprefix("check_"), FAIL, f"проверка упала: {type(e).__name__}: {e}")
    result.seconds = time.monotonic() - start
    print(f"  {MARK[result.status]} {result.name:<12} {result.summary}  ({result.seconds:.0f} с)", flush=True)
    return result


def write_report(task: dict | None, results: list[Result], green: bool, try_no: int) -> Path:
    out = reports_dir(task)
    rows = [f"# Отчёт: {task['id']} — {task['title']}" if task else "# Отчёт: базовая проверка",
            "", f"Ветка `{current_branch()}`, попытка {try_no}. Итог: **{'ЗЕЛЁНЫЙ' if green else 'КРАСНЫЙ'}**", "",
            "| Проверка | Итог | Кратко |", "|---|---|---|"]
    rows += [f"| {r.name} | {r.status} | {r.summary} |" for r in results]
    for r in results:
        if r.details.strip():
            rows += ["", f"## {r.name}", "```", r.details.strip()[-6000:], "```"]
    if task:
        cfg = load_config()
        new = [f for f in changed_files(cfg["base_branch"], only_added=True) if not f.startswith("reports/")]
        if new:
            rows += ["", "## Новые файлы", ""] + [f"- [{f}](../../{f})" for f in new]
        body = task["body"]
        if "## Чек-лист для глаз" in body:
            rows += ["", "## Чек-лист для глаз (проверяет человек)",
                     body.split("## Чек-лист для глаз", 1)[1].split("\n## ", 1)[0].split("\n", 1)[-1].strip()]
        q = out / "questions.md"
        if q.exists():
            rows += ["", "## Вопросы исполнителя", q.read_text(encoding="utf-8")]
    path = out / "report.md"
    path.write_text("\n".join(rows) + "\n", encoding="utf-8")
    return path


def verify(task_path: str | None, judge: bool, judge_model: str | None, try_no: int) -> bool:
    config = load_config()
    task = load_task(task_path) if task_path else None
    role = load_role(task["role"]) if task else None
    model = judge_model or (task or {}).get("judge_model") or config["judge_model"]
    ctx = Context(config=config, task=task, role=role, judge_model=model)
    results = [run_check(check_scope, ctx), run_check(check_tests, ctx)]
    if not task:
        pass
    elif not judge:
        results.append(Result("judge", SKIP, "судья выключен (--no-judge)"))
    elif any(r.status == FAIL for r in results):
        results.append(Result("judge", SKIP, "не запускался: есть красные проверки"))
    else:
        ctx.previous = list(results)
        results.append(run_check(check_judge, ctx))
    green = all(r.status != FAIL for r in results)
    out = reports_dir(task)
    (out / f"verify_try{try_no}.json").write_text(
        json.dumps({"green": green, "results": [asdict(r) for r in results]}, ensure_ascii=False, indent=2),
        encoding="utf-8")
    path = write_report(task, results, green, try_no)
    print(f"ИТОГ: {'ЗЕЛЁНЫЙ' if green else 'КРАСНЫЙ'} — отчёт {path.relative_to(ROOT).as_posix()}", flush=True)
    return green


def main() -> int:
    sys.stdout.reconfigure(encoding="utf-8")
    p = argparse.ArgumentParser(description="Проверка задания")
    p.add_argument("--task")
    p.add_argument("--baseline", action="store_true")
    p.add_argument("--no-judge", action="store_true")
    p.add_argument("--judge-model", choices=["opus", "sonnet"])
    p.add_argument("--try", dest="try_no", type=int, default=1)
    a = p.parse_args()
    if not a.task and not a.baseline:
        p.error("нужно --task или --baseline")
    return 0 if verify(a.task, not a.no_judge, a.judge_model, a.try_no) else 1


if __name__ == "__main__":
    sys.exit(main())
