#!/bin/bash

# get tag
tag="$1"

# verify tag format
if [[ ! "$tag" =~ ^v[0-9]+\.[0-9]+\.[0-9]+\+[0-9]+$ ]]; then
    echo "Invalid tag format. Correct format is vX.Y.Z+N."
    exit 1
fi

# check if tag already exists
if git tag --list | grep -q "^${tag}$"; then
    echo "Tag '$tag' already exists."
    exit 1
fi

# create tag and push
git tag "$tag"
git push origin "$tag"
