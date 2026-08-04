$ErrorActionPreference = "Stop"

$existing = docker ps -aq `
    --filter "name=chaerok-render-lambda-local"

if ($existing) {
    docker rm -f chaerok-render-lambda-local |
        Out-Null
}

docker run `
    --rm `
    --name chaerok-render-lambda-local `
    --platform linux/amd64 `
    --publish 9000:8080 `
    chaerok-render-lambda:smoke-v1
