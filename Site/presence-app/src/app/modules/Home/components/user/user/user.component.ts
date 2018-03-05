import { Component, OnInit, Input } from '@angular/core';
import { User } from '../../../../../shared/models/user.model';


@Component({
  selector: 'app-user',
  templateUrl: './user.component.html',
  styleUrls: ['./user.component.css']
})
export class UserComponent implements OnInit {

  @Input() public user: User;
  
  constructor() { }

  ngOnInit() {
  }

}
