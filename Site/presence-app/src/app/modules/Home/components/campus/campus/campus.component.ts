import { Component, OnInit, Input } from '@angular/core';
import { Campus } from '../../../../../shared/models/campus.model';
import { HomeDataService } from '../../../home.service';
import { User } from '../../../../../shared/models/user.model';

@Component({
  selector: 'app-campus',
  templateUrl: './campus.component.html',
  styleUrls: ['./campus.component.css']
})
export class CampusComponent implements OnInit {

  @Input() public campus: Campus;
  @Input() public users: User[];
  
  constructor() { }

  ngOnInit() {
    
  }

}
