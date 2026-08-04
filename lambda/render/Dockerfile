FROM gradle:8.14.3-jdk17 AS build

WORKDIR /workspace

COPY settings.gradle build.gradle ./
COPY src ./src

RUN gradle clean test classes --no-daemon

FROM public.ecr.aws/lambda/java:17

COPY --from=build /workspace/build/classes/java/main ${LAMBDA_TASK_ROOT}/
COPY --from=build /workspace/build/dependency/* ${LAMBDA_TASK_ROOT}/lib/

CMD ["com.chaerok.render.RenderHandler::handleRequest"]
