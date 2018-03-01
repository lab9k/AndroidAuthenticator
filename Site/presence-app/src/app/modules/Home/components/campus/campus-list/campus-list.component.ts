import { Component, OnInit } from '@angular/core';
import { HomeDataService } from '../../../home.service';
import { Campus } from '../../../../../shared/models/campus.model';

@Component({
  selector: 'app-campus-list',
  templateUrl: './campus-list.component.html',
  styleUrls: ['./campus-list.component.css']
})
export class CampusListComponent implements OnInit {

  private _campuses: Campus[];

  constructor(private _campusDataService: HomeDataService) { }

  ngOnInit() {
    this._campusDataService.campuses()
      .subscribe(items => {
        this._campuses = items;
      });
  }

  get campuses() {
    return this._campuses;
  }

}
