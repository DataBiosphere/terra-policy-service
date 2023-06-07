import json

from tests.smoke_test_case import SmokeTestCase


class VersionTests(SmokeTestCase):
    @staticmethod
    def version_url() -> str:
        return SmokeTestCase.build_service_url("/version")

    def test_status_code_is_200(self):
        response = SmokeTestCase.call_service(self.version_url())
        self.assertEqual(response.status_code, 200)

    def test_build_value_specified(self):
        response = SmokeTestCase.call_service(self.version_url())
        if response.status_code == 200:
            version = json.loads(response.text)
            self.assertIsNotNone(version["build"], "Version value must be non-empty")
        else:
            self.fail(f"Call to `/version` endpoint was not OK.  Response code: {response.status_code}")
