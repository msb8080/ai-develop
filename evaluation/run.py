#!/usr/bin/env python3
"""Run a small, repeatable SSE evaluation without exposing model credentials."""

from __future__ import annotations

import argparse
import json
import time
import urllib.request
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parent


def load_cases(limit: int | None) -> list[dict]:
    cases = [json.loads(line) for line in (ROOT / "cases.jsonl").read_text().splitlines() if line.strip()]
    return cases[:limit] if limit else cases


def run_case(base_url: str, project_id: str | None, case: dict) -> dict:
    payload = {"message": case["prompt"]}
    if project_id:
        payload["projectId"] = project_id
    if case.get("skillId"):
        payload["skillId"] = case["skillId"]
    request = urllib.request.Request(
        f"{base_url}/api/chat/stream",
        data=json.dumps(payload).encode(),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    started = time.monotonic()
    answer: list[str] = []
    metadata: dict = {}
    error: dict = {}
    with urllib.request.urlopen(request, timeout=180) as response:
        for raw in response:
            line = raw.decode().strip()
            if not line.startswith("data:"):
                continue
            event = json.loads(line[5:])
            if event.get("provider"):
                metadata = event
            if event.get("content"):
                answer.append(event["content"])
            if event.get("code"):
                error = event
    content = "".join(answer)
    required = case.get("required", [])
    return {
        "id": case["id"],
        "category": case["category"],
        "passed": not error and all(term.lower() in content.lower() for term in required),
        "latencyMs": round((time.monotonic() - started) * 1000),
        "provider": metadata.get("provider"),
        "model": metadata.get("model"),
        "skill": metadata.get("skillId"),
        "contextSourceCount": len(metadata.get("contextSources", [])),
        "required": required,
        "error": error.get("code"),
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--project-id")
    parser.add_argument("--limit", type=int)
    args = parser.parse_args()
    cases = load_cases(args.limit)
    results = [run_case(args.base_url.rstrip("/"), args.project_id, case) for case in cases]
    report = {
        "createdAt": datetime.now(timezone.utc).isoformat(),
        "passed": sum(item["passed"] for item in results),
        "total": len(results),
        "results": results,
    }
    output_dir = ROOT / "reports"
    output_dir.mkdir(exist_ok=True)
    output = output_dir / f"report-{datetime.now().strftime('%Y%m%d-%H%M%S')}.json"
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2))
    print(json.dumps({"passed": report["passed"], "total": report["total"], "report": str(output)}))


if __name__ == "__main__":
    main()
