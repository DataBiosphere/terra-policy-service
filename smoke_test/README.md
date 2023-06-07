# TPS Smoke Tests

These smoke tests provide a means for running a small set of tests against a live running TPS instance to validate that
it is up and functional.  Ideally, these tests should verify more than the `/status` endpoint and should additionally try to 
verify some basic functionality.  

These tests should run quickly (no longer than a few seconds), should be idempotent, and when possible, should not 
make any changes to the state of the service or its data.  

## Requirements

Python 3.10.3 or higher

## Setup

You will need to install required pip libraries:

```pip install -r requirements.txt```

## Run

To run the smoke tests:

```python smoke_test.py {TPS_HOST}```

For example:

```python smoke_test.py tps.dsde-dev.broadinstitute.org```

## Required and Optional Arguments

### TPS_HOST
Required - Can be just a domain or a domain and port, for example:

* `tps.dsde-dev.broadinstitute.org`
* `tps.dsde-dev.broadinstitute.org:443`

The protocol can also be added if you desire, however, most TPS instance can and should use HTTPS and this is the
default if no protocol is specified:

* `http://tps.dsde-dev.broadinstitute.org`
* `https://tps.dsde-dev.broadinstitute.org`

### Verbosity
Optional - You may control how much information is printed to `STDOUT` while running the smoke tests by passing a 
verbosity argument to `smoke_test.py`.  For example to print more information about the tests being run, use the `-v`
argument with a verbosity level of `0`, `1`, or `2`, for example:

```python -v 2 smoke_test.py {TPS_HOST}```