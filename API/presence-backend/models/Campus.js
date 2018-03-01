let mongoose = require('mongoose')

let CampusSchema = new mongoose.Schema({
    _id: String,
    locations: [
        {
            type: String,
            ref: 'Location'
        },
    ]
});

mongoose.model('Campus', CampusSchema);