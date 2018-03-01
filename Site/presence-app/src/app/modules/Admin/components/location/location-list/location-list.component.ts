import { Component, OnInit, Input, OnDestroy } from '@angular/core';
import { Location } from '../../../../../shared/models/location.model';
import { DragulaService } from 'ng2-dragula';
import { AdminDataService } from '../../../admin.service';
import { Campus } from '../../../../../shared/models/campus.model';
import { log } from 'util';

@Component({
  selector: 'app-location-list',
  templateUrl: './location-list.component.html',
  styleUrls: ['./location-list.component.css']
})
export class LocationListComponent implements OnInit, OnDestroy {

  @Input() public campus: Campus;
  private _locations: Location[];
  private dropDragSubscription;
  private dropDropSubscription;
  private dropCancelSubscription;
  
  constructor(private dragulaService: DragulaService, private adminDataService: AdminDataService) { 
    this.dropDragSubscription = dragulaService.drag.subscribe((value) => {
      this.onDrag(value.slice(1));
    });
    this.dropDropSubscription = dragulaService.drop.subscribe((value) => {
      this.onDrop(value.slice(1));
    });
    this.dropCancelSubscription = dragulaService.cancel.subscribe((value) => {
      this.onCancel(value.slice(1));
    });
  }

  ngOnInit() {
    this._locations = [];
    this.campus.locations.forEach(loc => {
      this._locations.push(loc);
    })
  }

  ngOnDestroy() {
    this.dropDragSubscription.unsubscribe();
    this.dropDropSubscription.unsubscribe();
    this.dropCancelSubscription.unsubscribe();
  }

  //item removed
  private onDrag(args) {
    let [e, el] = args;
    if(el.id === this.campus.name) {
      let loc = this._locations.filter(location =>
        location.id === e.id)[0];
      let index = this._locations.indexOf(loc);
      this._locations.splice(index,1);
      console.log("remove " + this.campus.name + " " +  loc.id);
      this.adminDataService.removeLocation(this.campus.name, loc.id).subscribe();
    }
  }
  
  //item dropped
  private onDrop(args) {
    let [e, el] = args;
    let l;
    if(el.id === this.campus.name) {
      
      this.adminDataService.getLocation(e.id).subscribe(items => {
        l = items;
        console.log(l);
        this._locations.push(l);
        console.log("add " + this.campus.name + " " + l.id);
        this.adminDataService.addLocation(this.campus.name, l.id).subscribe();
      });
    
    }
  }

  //item removed, but came back
  private onCancel(args) {
    let [e, el] = args;
    if(el.id === this.campus.name) {
      let l;
      this.adminDataService.getLocation(e.id).subscribe(items => {
        l = items;
        console.log(l);
        this._locations.push(l);
        console.log("add " + this.campus.name + " " + l.id);
        this.adminDataService.addLocation(this.campus.name, l.id).subscribe();
      });
    }
  }

  get locations() {
    return this._locations;
  }
}