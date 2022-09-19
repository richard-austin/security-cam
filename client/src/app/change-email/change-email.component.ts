import {Component, OnInit, ViewChild} from '@angular/core';
import {AbstractControl, FormControl, FormGroup, Validators} from '@angular/forms';
import {ReportingComponent} from '../reporting/reporting.component';
import {HttpErrorResponse} from '@angular/common/http';
import { UtilsService } from '../shared/utils.service';
import { AfterViewInit } from '@angular/core';

@Component({
  selector: 'app-change-email',
  templateUrl: './change-email.component.html',
  styleUrls: ['./change-email.component.scss']
})
export class ChangeEmailComponent implements OnInit, AfterViewInit {

  changeEmailForm!: FormGroup;
  @ViewChild(ReportingComponent) reporting!:ReportingComponent;

  constructor(private utilsService:UtilsService) { }

  changeEmailButtonDisabled():boolean {
    return this.anyInvalid();
  }

  hasError = (controlName: string, errorName: string):boolean =>{
    return this.changeEmailForm.controls[controlName].hasError(errorName);
  }

  formSubmitted() {
    let password:AbstractControl = this.changeEmailForm.controls['password'];
    let newEmail:AbstractControl = this.changeEmailForm.controls['newEmail'];
    let confirmNewEmail: AbstractControl = this.changeEmailForm.controls['confirmNewEmail'];

    this.utilsService.changeEmail(password.value, newEmail.value, confirmNewEmail.value).subscribe(() => {
        this.reporting.successMessage="Email changed";
      },
      (reason: HttpErrorResponse) => {
        if(reason.status === 400)
        {
          for(const key of Object.keys(reason.error)) {
            if(key === 'password')
              this.invalidPassword();
          }
          this.reporting.errorMessage = reason;
        }
        else
          this.reporting.errorMessage = reason;
      });
  }

  invalidPassword()
  {
    let passwordCtl: AbstractControl = this.changeEmailForm.controls['password'];
    let errors:{[key: string]: any} = {pattern: {badPassword:"Password Incorrect"}};
    passwordCtl.setErrors(errors);
    passwordCtl.markAsTouched({onlySelf: true});
  }

  /**
   * compareEmails: Custom form field validator to check new email and confirm new eamil
   *                   are equal
   * @param control
   */
  compareEmails(control: AbstractControl): { [key: string]: boolean } | null {
    let fg: FormGroup=control.parent as FormGroup;
    let ac: AbstractControl = fg?.controls['newEmail'];
    if (control.value !== undefined && control.value !== ac?.value) {
      return { 'confirmNewEmail': true };
    }
    return null;
  }

  anyInvalid(): boolean{
    return this.changeEmailForm.invalid;
  }

  ngOnInit(): void {
    this.changeEmailForm = new FormGroup({
      password: new FormControl('', [Validators.required]),
      newEmail: new FormControl('', [Validators.required, Validators.pattern(RegExp("^([a-zA-Z0-9_\\-.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(]?)$"))]),
      confirmNewEmail: new FormControl('', [Validators.required, this.compareEmails])
    });
    this.changeEmailForm.markAllAsTouched();
  }

  ngAfterViewInit(): void {
    this.utilsService.getEmail().subscribe((result)=> {
      this.changeEmailForm.controls['newEmail'].setValue(result.email);
    })
  }
}
