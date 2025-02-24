import {Component, OnInit, ViewChild} from '@angular/core';
import {AbstractControl, UntypedFormControl, UntypedFormGroup, Validators} from "@angular/forms";
import {ReportingComponent} from "../reporting/reporting.component";
import {HttpErrorResponse} from "@angular/common/http";
import {UtilsService} from '../shared/utils.service';
import {MatCheckboxChange} from '@angular/material/checkbox';
import {SharedModule} from "../shared/shared.module";
import {SharedAngularMaterialModule} from "../shared/shared-angular-material/shared-angular-material.module";

@Component({
    selector: 'app-set-up-guest-account',
    templateUrl: './set-up-guest-account.component.html',
    styleUrls: ['./set-up-guest-account.component.scss'],
    imports: [SharedModule, SharedAngularMaterialModule]
})
export class SetUpGuestAccountComponent implements OnInit {
    setupGuestAccountForm!: UntypedFormGroup;
    private originalEnabledState!: boolean
    @ViewChild(ReportingComponent) reporting!: ReportingComponent;
    private currentEnabledState!: boolean;

    constructor(private utilsService: UtilsService) {
    }

    submitButtonDisabled(): boolean {
        let password: string = this.setupGuestAccountForm.controls['password'].value;

        return this.anyInvalid() ||
            ((this.originalEnabledState === this.currentEnabledState) &&
                (password === undefined || password === ''));
    }

    hasError = (controlName: string, errorName: string): boolean => {
        return this.setupGuestAccountForm.controls[controlName].hasError(errorName);
    }

    formSubmitted() {
        let enabled: AbstractControl = this.setupGuestAccountForm.controls['enabled'];
        let password: AbstractControl = this.setupGuestAccountForm.controls['password'];
        let confirmPassword: AbstractControl = this.setupGuestAccountForm.controls['confirmPassword'];

        this.currentEnabledState = this.originalEnabledState = enabled.value;

        this.utilsService.setupGuestAccount(enabled.value, password.value, confirmPassword.value).subscribe(() => {
                this.reporting.successMessage = "Guest account settings changed";

                // Clear the password fields
                this.setupGuestAccountForm.controls['password'].setValue('');
                this.setupGuestAccountForm.controls['confirmPassword'].setValue('');
            },
            (reason: HttpErrorResponse) => {
                if (reason.status === 400) {
                    for (const key of Object.keys(reason.error)) {
                        if (key === 'password')
                            this.invalidPassword();
                    }
                    this.reporting.errorMessage = reason;
                } else
                    this.reporting.errorMessage = reason;
            });
    }

    invalidPassword() {
        let passwordCtl: AbstractControl = this.setupGuestAccountForm.controls['password'];
        let errors: { [key: string]: any } = {pattern: {badPassword: "Password Incorrect"}};
        passwordCtl.setErrors(errors);
        passwordCtl.markAsTouched({onlySelf: true}); //updateValueAndValidity({onlySelf: true, emitEvent: true});
    }

    /**
     * comparePasswords: Custom form field validator to check new password and confirm new password
     *                   are equal
     * @param control
     */
    comparePasswords(control: AbstractControl): { [key: string]: boolean } | null {
        let fg: UntypedFormGroup = control.parent as UntypedFormGroup;
        let ac: AbstractControl = fg?.controls['password'];
        if (control.value !== undefined && control.value !== ac?.value) {
            return {'confirmPassword': true};
        }
        return null;
    }

    anyInvalid(): boolean {
        return this.setupGuestAccountForm.invalid;
    }

    setEnabled($event: MatCheckboxChange) {
        this.currentEnabledState = $event.checked
        this.setupGuestAccountForm.controls['enabled'].setValue($event.checked);
    }

    async ngOnInit(): Promise<void> {
        this.setupGuestAccountForm = new UntypedFormGroup({
            enabled: new UntypedFormControl('', []),
            password: new UntypedFormControl('', [Validators.pattern(/^$|^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,64}$/)]),
            confirmPassword: new UntypedFormControl('', [this.comparePasswords])
        }, {updateOn: "change"});

        // Trigger revalidation on confirmPassword when password is changed.
        this.setupGuestAccountForm.controls['password'].valueChanges.subscribe(() => {
                this.setupGuestAccountForm.controls['confirmPassword'].updateValueAndValidity();
            }
        )
        this.setupGuestAccountForm.markAllAsTouched();
        this.originalEnabledState = this.currentEnabledState = (await this.utilsService.guestAccountEnabled()).enabled
        this.setupGuestAccountForm.controls['enabled'].setValue(this.originalEnabledState);
    }
}
