FROM python:3

ARG version

WORKDIR /app

ADD ./app/ ./
ADD ./requirements.txt ./

RUN pip install --no-cache-dir -r requirements.txt

ENTRYPOINT python app.py runserver

