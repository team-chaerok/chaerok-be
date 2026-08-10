FROM mwader/static-ffmpeg:7.1.1-amd64@sha256:6769881cc02c80d33e387750a8e144d162adfab2775e934dd97899261dda3a0c AS ffmpeg

FROM gradle:8.14.3-jdk17 AS build

WORKDIR /workspace

COPY lambda/render/settings.gradle lambda/render/build.gradle ./lambda/render/
COPY lambda/render/src ./lambda/render/src
COPY src/main/java/com/chaerok/backend/filter ./src/main/java/com/chaerok/backend/filter
COPY src/main/resources/filter ./src/main/resources/filter

RUN gradle -p lambda/render clean test classes --no-daemon

FROM public.ecr.aws/lambda/java:17

ENV JAVA_TOOL_OPTIONS="-Djava.awt.headless=true" \
    FFMPEG_PATH="/opt/bin/ffmpeg"

COPY --from=ffmpeg /ffmpeg /opt/bin/ffmpeg
COPY --from=build /workspace/lambda/render/build/classes/java/main ${LAMBDA_TASK_ROOT}/
COPY --from=build /workspace/lambda/render/build/resources/main ${LAMBDA_TASK_ROOT}/
COPY --from=build /workspace/lambda/render/build/dependency/* ${LAMBDA_TASK_ROOT}/lib/

CMD ["com.chaerok.render.RenderHandler::handleRequest"]
