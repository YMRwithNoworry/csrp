#!/usr/bin/env bash
set -e
git add scripts src
git commit -m '修复护甲纹理与运行时资源兼容'
git push origin port-1.20.1-forge
