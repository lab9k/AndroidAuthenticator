import { Component, OnInit } from '@angular/core';
import { HomeDataService } from '../../../home.service';
import { Campus } from '../../../../../shared/models/campus.model';
import { User } from '../../../../../shared/models/user.model';

@Component({
  selector: 'app-campus-list',
  templateUrl: './campus-list.component.html',
  styleUrls: ['./campus-list.component.css']
})
export class CampusListComponent implements OnInit {

  private _campuses: Campus[];
  private _users: User[];

  constructor(private _homeDataService: HomeDataService) { }

  ngOnInit() {
    this._homeDataService.campuses()
      .subscribe(items => this._campuses = items);
    this._homeDataService.users()
      .subscribe(items => this._users = items);
  }

  get campuses() {
    return this._campuses;
  }

  get users() {
    return this._users;
  }

}
