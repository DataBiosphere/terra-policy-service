import argparse
import sys
import unittest
from unittest import TestSuite

from tests.smoke_test_case import SmokeTestCase
from tests.status_tests import StatusTests
from tests.version_tests import VersionTests

DESCRIPTION = """
TPS Smoke Test
Enter the host (domain and optional port) of the service you want to to test.  This test will ensure that the TPS 
instance running on that host is minimally functional.
"""


def gather_tests() -> TestSuite:
    suite = unittest.TestSuite()

    status_tests = unittest.defaultTestLoader.loadTestsFromTestCase(StatusTests)
    version_tests = unittest.defaultTestLoader.loadTestsFromTestCase(VersionTests)

    suite.addTests(status_tests)
    suite.addTests(version_tests)

    return suite


def main(main_args):
    SmokeTestCase.HOSTNAME = main_args.hostname

    test_suite = gather_tests()

    runner = unittest.TextTestRunner(verbosity=main_args.verbosity)
    runner.run(test_suite)


if __name__ == "__main__":
    try:
        parser = argparse.ArgumentParser(
            description=DESCRIPTION,
            formatter_class=argparse.RawTextHelpFormatter
        )
        parser.add_argument(
            "-v",
            "--verbosity",
            type=int,
            choices=[0, 1, 2],
            default=1,
            help="""Python unittest verbosity setting: 
0: Quiet - Prints only number of tests executed
1: Minimal - (default) Prints number of tests executed plus a dot for each success and an F for each failure
2: Verbose - Help string and its result will be printed for each test"""
        )
        parser.add_argument(
            "hostname",
            help="domain with optional port number of the TPS host you want to test"
        )

        args = parser.parse_args()

        # Need to pop off sys.argv values to avoid messing with args passed to unittest.main()
        for _ in range(len(sys.argv[1:])):
            sys.argv.pop()

        main(args)
        sys.exit(0)

    except Exception as e:
        print(e)
        sys.exit(1)
