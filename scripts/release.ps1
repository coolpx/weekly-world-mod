# get tag
$tag = $args[0];

# verify tag format
if ($tag -notmatch '^v\d+\.\d+\.\d+\+\d+$') {
    Write-Host "Invalid tag format. Correct format is vX.Y.Z+N."
    exit 1
}

# check if tag already exists
$existingTags = git tag --list $tag
if ($existingTags) {
    Write-Host "Tag '$tag' already exists."
    exit 1
}

# push changes to main branch
git push origin main

# create tag and push
git tag $tag
git push origin $tag
