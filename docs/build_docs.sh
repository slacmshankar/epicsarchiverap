#! /bin/bash
if ! command -v sphinx-build &> /dev/null
then
    echo "Instaling sphinx-build in a new virtual environment"
    python -m venv .venv
    source .venv/bin/activate
    python -m pip install --upgrade --no-cache-dir pip setuptools
    python -m pip install --upgrade --no-cache-dir .
else
    echo "Using sphinx-build from the environment"
fi
sphinx-build docs/source docs/build