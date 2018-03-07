let mongoose = require('mongoose')

let LocationSchema = new mongoose.Schema({
    name: String,
    stickers: [
        {
            type: String
        }
    ]
});

mongoose.model('Location', LocationSchema);