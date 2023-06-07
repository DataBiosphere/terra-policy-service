import re
from functools import cache
from unittest import TestCase
from urllib.parse import urljoin

import requests
from requests import Response


class SmokeTestCase(TestCase):
    HOSTNAME = None

    @staticmethod
    def build_service_url(path: str) -> str:
        assert SmokeTestCase.HOSTNAME, "ERROR - SmokeTests.HOSTNAME not properly set"
        if re.match(r"^\s*https?://", SmokeTestCase.HOSTNAME):
            return urljoin(SmokeTestCase.HOSTNAME, path)
        else:
            return urljoin(f"https://{SmokeTestCase.HOSTNAME}", path)

    @staticmethod
    @cache
    def call_service(url: str, user_token: str = None) -> Response:
        """Function is memoized so that we only make the call once"""
        headers = {"Authorization": f"Bearer {user_token}"} if user_token else {}
        return requests.get(url, headers=headers)
