import { Component, OnInit, Input } from '@angular/core';
import { Campus } from '../../../../../shared/models/campus.model';

@Component({
  selector: 'app-campus-list',
  templateUrl: './campus-list.component.html',
  styleUrls: ['./campus-list.component.css']
})
export class CampusListComponent implements OnInit {

  @Input() public campuses: Campus[];
  
  constructor() { }

  ngOnInit() {
  }

}
