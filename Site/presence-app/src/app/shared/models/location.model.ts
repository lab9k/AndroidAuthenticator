import { Checkin } from "./checkin.model";

export class Location {
    private _id: string;
    private _name: string;

    static fromJSON(json) {
        if(json.id !== undefined) {
            json._id = json.id;
        }
        const location = new Location(json._id, json.name);
        return location;
    }

    constructor(id: string, name: string) {
        this._id = id;
        this._name = name;
    }

    get id() {
        return this._id;
    }

    get name() {
        return this._name;
    }
}