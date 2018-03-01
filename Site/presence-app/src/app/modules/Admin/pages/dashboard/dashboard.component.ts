import { Component, OnInit } from '@angular/core';
import { Campus } from '../../../../shared/models/campus.model';
import { Location } from '../../../../shared/models/location.model';
import { AdminDataService } from '../../admin.service';
import { DragulaService } from 'ng2-dragula/components/dragula.provider';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
})
export class DashboardComponent implements OnInit {

  private _campuses: Campus[];
  private _campus: Campus;

  constructor(private _adminDataService: AdminDataService, private dragulaService: DragulaService) { 
    
  }

  ngOnInit() {
    this._adminDataService.campuses()
    .subscribe(items => {
      this._campus = items.filter( campus =>
        campus.name === "Not in a campus")[0];
      this._campuses = items.filter( campus =>
        campus.name !== "Not in a campus");
    });
  }

  get campuses() {
    return this._campuses;
  }

  get campus() {
    return this._campus;
  }

}
