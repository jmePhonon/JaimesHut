#!/bin/bash
mkdir -p tmp
mkdir -p src/main/resources/
curl "https://ci-deploy.frk.wf/JaimesHut/res.zip" --output tmp/res.zip
unzip -o tmp/res.zip -d src/main/resources/
