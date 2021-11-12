import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup, Validators} from "@angular/forms";

@Component({
  selector: 'app-credentials-for-camera-access',
  templateUrl: './credentials-for-camera-access.component.html',
  styleUrls: ['./credentials-for-camera-access.component.scss']
})
export class CredentialsForCameraAccessComponent implements OnInit {

  @Output() hideDialogue: EventEmitter<void> = new EventEmitter<void>();
  constructor() { }

  camerasUsername: string = '';
  camerasPassword: string = '';
  setPasswordForm!: FormGroup;


  hidePasswordDialogue() {
    this.hideDialogue.emit();
  }

  updateCredentials() {
    let x = this.camerasUsername;
    let y= this.camerasPassword;
    let z = y;
  }

  ngOnInit(): void {

    this.setPasswordForm = new FormGroup({
      camerasUsername: new FormControl('', [Validators.required, Validators.maxLength(25)]),
      camerasPassword: new FormControl('', [Validators.required, Validators.maxLength(25)])
    })

  }

}
