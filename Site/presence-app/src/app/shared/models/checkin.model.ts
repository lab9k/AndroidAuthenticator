import { User } from "./user.model";

export class Checkin {
    private _time: number;
    private _user: User;

    static fromJSON(json): Checkin {
        const checkin = new Checkin(json.time, json.user);
        return checkin;
    }

    constructor(time: number, user: User) {
        this._time =  time;
        this._user = user;
    }

    get time() {
        return this._time;
    }

    get user() {
        return this._user;
    }
}