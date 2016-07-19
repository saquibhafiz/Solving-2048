var express = require('express');
var app = require('express')();
var server = require('http').Server(app);
var io = require('socket.io')(server);
var board = io.of('/board');
var bot = io.of('/bot');

app.use("/js", express.static(__dirname + '/js'));
app.use("/meta", express.static(__dirname + '/meta'));
app.use("/style", express.static(__dirname + '/style'));

app.get('/', function(req, res){
  res.sendFile('/index.html' , { root : __dirname});
});

server.listen(80);

board.on('connection', function (socket) {
  socket.on('result', function (gameboard) {
    bot.emit('result', gameboard);
  });
});

bot.on('connection', function (socket) {
  board.emit('startboard');
  socket.on('move', function (move) {
    board.emit('move', move);
  });
});
