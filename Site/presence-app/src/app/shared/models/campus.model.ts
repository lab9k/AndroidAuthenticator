import { Location } from './location.model'

export class Campus {
    private _id: string;
    private _name: string;
    private _locations: Location[]; 

    static fromJSON(json) {
        let locs = [];
        json.locations.forEach(element => {
            locs.push(Location.fromJSON(element));
        });
        const campus = new Campus(json._id, locs);
        return campus;
    }

    constructor(name: string, locations: Location[]) {
        this._name = name;
        this._locations = locations;
    }

    get id(): string {
        return this._id;
    }

    get name(): string {
        return this._name;
    }

    set name(name: string) {
        this._name = name;
    }
    
    get locations() {
        return this._locations;
    }

    addLocation(loc: Location) {
        this._locations.push(loc);
    }

    removeLocation(loc: Location) {
        const index = this._locations.indexOf(loc);
        if(index !== -1)  {
            this._locations.splice(index, 1);
        }
    }
}