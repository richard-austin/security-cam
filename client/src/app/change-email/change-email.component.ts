import {Component, OnInit, ViewChild} from '@angular/core';
import {AbstractControl, UntypedFormControl, UntypedFormGroup, Validators} from '@angular/forms';
import {ReportingComponent} from '../reporting/reporting.component';
import { HttpErrorResponse } from '@angular/common/http';
import { UtilsService } from '../shared/utils.service';
import { AfterViewInit } from '@angular/core';
import {SharedModule} from "../shared/shared.module";
import {SharedAngularMaterialModule} from "../shared/shared-angular-material/shared-angular-material.module";

@Component({
  selector: 'app-change-email',
  templateUrl: './change-email.component.html',
  styleUrls: ['./change-email.component.scss'],
  imports:[SharedModule, SharedAngularMaterialModule]
})
export class ChangeEmailComponent implements OnInit, AfterViewInit {

  changeEmailForm!: UntypedFormGroup;
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
    let fg: UntypedFormGroup=control.parent as UntypedFormGroup;
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
    this.changeEmailForm = new UntypedFormGroup({
      password: new UntypedFormControl('', [Validators.required]),
      newEmail: new UntypedFormControl('', [Validators.required, Validators.pattern(RegExp("^([a-zA-Z0-9_\\-.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(]?)$"))]),
      confirmNewEmail: new UntypedFormControl('', [Validators.required, this.compareEmails])
    });
    this.changeEmailForm.markAllAsTouched();
  }

  ngAfterViewInit(): void {
    this.utilsService.getEmail().subscribe((result)=> {
      this.changeEmailForm.controls['newEmail'].setValue(result.email);
    })
  }

  protected readonly UtilsService = UtilsService;
}
