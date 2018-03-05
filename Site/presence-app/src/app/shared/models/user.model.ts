export class User {
    private _id: string;
    private _name: string;
    private _checkin: {
        location: string;
        time: number;
    };
    private _picture: string;

    static fromJSON(json) {
        const user = new User(json._id, json.name, json.checkin, json.picture);
        return user;
    }
    constructor(id: string, name: string, checkin: {location: string;time: number;}, picture: string) {
        this._id = id;
        this._name = name;
        this._checkin = checkin;
        this._picture = picture;
    }

    get id() {
        return this._id;
    }

    get name() {
        return this._name;
    }

    set name(name: string) {
        this._name = name;
    }

    get checkin() {
        return this._checkin;
    }

    get picture() {
        return this._picture;
    }
}