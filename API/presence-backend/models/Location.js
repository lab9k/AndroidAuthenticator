let mongoose = require('mongoose')

let LocationSchema = new mongoose.Schema({
    _id: String,
    name: String,
});

mongoose.model('Location', LocationSchema);