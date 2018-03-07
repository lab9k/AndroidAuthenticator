let mongoose = require('mongoose');

let UserSchema = new mongoose.Schema({
    _id: String,
    name: String,
    checkin: {
        location: {
            type: mongoose.Schema.Types.ObjectId,
            ref: 'Location',
        },
        time: Number,
    },
    picture: String,
});

mongoose.model('User', UserSchema);