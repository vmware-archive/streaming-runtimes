FROM python:3-alpine

ARG USER=1001

RUN adduser -h /usr/src/mkdocs -D -u $USER mkdocs \
&& apk add bash \
&& apk add git

ENV PATH="${PATH}:/usr/src/mkdocs/.local/bin"

USER mkdocs
RUN mkdir -p /usr/src/mkdocs/build
WORKDIR /usr/src/mkdocs/build

RUN pip install --upgrade pip

RUN pip install pymdown-extensions \
&& pip install mkdocs \
&& pip install mkdocs-material \
&& pip install mkdocs-rtd-dropdown \
&& pip install mkdocs-git-revision-date-plugin \
&& pip install mkdocs-git-revision-date-localized-plugin

COPY ./streaming-runtime-samples ./streaming-runtime-samples
COPY ./material ./material
COPY mkdocs.yml .

EXPOSE 8000

ENTRYPOINT ["mkdocs", "serve"]
