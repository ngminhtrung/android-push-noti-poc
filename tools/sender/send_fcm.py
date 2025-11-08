#!/usr/bin/env python3
"""Minimal FCM HTTP v1 sender for local testing."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any, Dict

import requests
from google.auth.transport.requests import Request
from google.oauth2 import service_account

SCOPES = ["https://www.googleapis.com/auth/firebase.messaging"]


def parse_data(items: list[str]) -> dict[str, str]:
    parsed: dict[str, str] = {}
    for item in items or []:
        if "=" not in item:
            raise argparse.ArgumentTypeError(
                f"Data item '{item}' must be in key=value format"
            )
        key, value = item.split("=", 1)
        parsed[key] = value
    return parsed


def load_config(path: Path | None) -> Dict[str, Any]:
    if path is None:
        return {}
    if not path.exists():
        return {}
    try:
        with path.open("r", encoding="utf-8") as handle:
            data = json.load(handle)
            if isinstance(data, dict):
                return data
    except json.JSONDecodeError as exc:  # pragma: no cover - config errors are user facing
        raise SystemExit(f"Invalid JSON in config file {path}: {exc}") from exc
    raise SystemExit(f"Config file {path} must contain a JSON object")


def get_access_token(service_account_path: Path) -> str:
    credentials = service_account.Credentials.from_service_account_file(
        str(service_account_path), scopes=SCOPES
    )
    authed = credentials.with_scopes(SCOPES)
    authed.refresh(Request())
    if not authed.token:
        raise RuntimeError("Failed to obtain OAuth token")
    return authed.token


def send_message(project_id: str, token: str, body: dict, access_token: str) -> requests.Response:
    url = f"https://fcm.googleapis.com/v1/projects/{project_id}/messages:send"
    headers = {
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json; UTF-8",
    }
    return requests.post(url, headers=headers, json=body, timeout=10)


def main() -> int:
    default_config = Path(__file__).with_name("sender-config.json")
    parser = argparse.ArgumentParser(description="Send a test FCM push via HTTP v1 API")
    parser.add_argument(
        "--config",
        default=str(default_config),
        help="Path to sender-config.json (set to '' to skip looking for a file)",
    )
    parser.add_argument("--service-account", help="Path to service-account.json")
    parser.add_argument("--project-id", help="Firebase/GCP project id")
    parser.add_argument("--token", help="FCM registration token from the device log")
    parser.add_argument("--title", default=None, help="Notification title")
    parser.add_argument("--body", default=None, help="Notification body text")
    parser.add_argument(
        "--data",
        nargs="*",
        default=[],
        metavar="key=value",
        help="Optional data payload entries"
    )

    args = parser.parse_args()
    config_path = Path(args.config).expanduser() if args.config else None
    config = load_config(config_path)

    service_account_value = args.service_account or config.get("service_account")
    project_id = args.project_id or config.get("project_id")
    token_arg = args.token or config.get("token")
    title = args.title or config.get("title") or "Hello from PoC"
    body = args.body or config.get("body") or "This came from the local sender."

    if not service_account_value:
        parser.error("--service-account is required (set in CLI or config file)")
    if not project_id:
        parser.error("--project-id is required (set in CLI or config file)")
    if not token_arg:
        parser.error("--token is required (set in CLI or config file)")

    service_account_path = Path(service_account_value).expanduser()
    if not service_account_path.exists():
        parser.error(f"Service account file not found: {service_account_path}")

    config_data = config.get("data", {})
    if isinstance(config_data, dict):
        data_payload = dict(config_data)
    else:
        data_payload = {}
    data_payload.update(parse_data(args.data))
    message: dict = {
        "token": token_arg,
        "data": data_payload,
    }
    if title or body:
        message["notification"] = {"title": title, "body": body}

    payload = {"message": message}

    try:
        token = get_access_token(service_account_path)
    except Exception as exc:  # pragma: no cover - for local diagnostics only
        print(f"Failed to get access token: {exc}", file=sys.stderr)
        return 1

    response = send_message(project_id, token_arg, payload, token)
    if response.ok:
        print(json.dumps(response.json(), indent=2))
        return 0

    print(f"Request failed ({response.status_code}): {response.text}", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
