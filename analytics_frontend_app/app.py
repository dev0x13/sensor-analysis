# -*- coding: utf-8 -*-

from flask import Flask, request, url_for, render_template, jsonify
from flask_bootstrap import Bootstrap
from config import DefaultConfig
from db import Db
from datetime import datetime

app = Flask(__name__)

Bootstrap(app)

app.config.from_object(DefaultConfig)

db = Db()

@app.route("/", methods=["GET"])
def index():
    return render_template("index.html")


@app.route("/getUsersOnline", methods=["GET"])
def getUsersOnline():
    usersOnline = db.getAllItems("users_states")["Items"]

    for u in usersOnline:
        u["username"]["S"] = u["username"]["S"].split("_")

    return jsonify({'usersOnline': usersOnline})


if __name__ == "__main__":
    app.run("0.0.0.0")
