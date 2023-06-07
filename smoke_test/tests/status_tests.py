import json

from tests.smoke_test_case import SmokeTestCase


class StatusTests(SmokeTestCase):
    @staticmethod
    def status_url() -> str:
        return SmokeTestCase.build_service_url("/status")

    def test_status_code_is_200(self):
        response = SmokeTestCase.call_service(self.status_url())
        self.assertEqual(response.status_code, 200)
