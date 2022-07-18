# syntax=docker/dockerfile:1

FROM node:18.4.0
ENV NODE_ENV=production

WORKDIR /app

COPY ["package.json", "package-lock.json*", "./"]

RUN npm install --production

COPY aggregate.js .

CMD [ "node", "aggregate.js" ]