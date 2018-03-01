export class User {
    private _id: string;
    private _name: string;
    private _checkin: {
        location: string;
        time: number;
    };

    static fromJSON(json) {
        const user = new User(json._id, json.name, json.checkin);
        return user;
    }
    constructor(id: string, name: string, checkin: {location: string;time: number;}) {
        this._id = id;
        this._name = name;
        this._checkin = checkin;
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
}