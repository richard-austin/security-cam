import {AfterViewInit, Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {CameraService} from "../../cameras/camera.service";
import {Camera} from "../../cameras/Camera";
import {ReportingComponent} from "../../reporting/reporting.component";
import {BehaviorSubject} from "rxjs";
import {UntypedFormArray, UntypedFormControl, UntypedFormGroup, Validators} from "@angular/forms";
import {SharedModule} from "../../shared/shared.module";
import {SharedAngularMaterialModule} from "../../shared/shared-angular-material/shared-angular-material.module";
import {UtilsService} from "../../shared/utils.service";
@Component({
  selector: 'app-onvif-failures',
  templateUrl: './onvif-failures.component.html',
  styleUrls: ['./onvif-failures.component.scss'],
  imports: [
    SharedModule,
    SharedAngularMaterialModule
  ],
  standalone: true
})
export class OnvifFailuresComponent implements OnInit, AfterViewInit {
  @Input() failures!: Map<string, string>;
  @Input() cameras!: Map<string, Camera>;
  @Input() reporting!: ReportingComponent;
  @Output() fixUpCamerasData: EventEmitter<void> = new EventEmitter<void>();

  list$!: BehaviorSubject<[string, string][]>;
  failControls!: UntypedFormArray;
  onvifUserName: string = "";
  onvifPassword: string = "";
  gettingCameraDetails: boolean = false;
  onvifUrl!: string;

  readonly displayedColumns: string[] = ['onvifUrl', 'error', 'onvifUser', 'onvifPassword', 'discover'];

  constructor(private cameraSvc: CameraService) {
  }

  discover(onvifUrl: string, onvifUserName: string, onvifPassword: string) {
    this.gettingCameraDetails = true;
    this.onvifUrl = onvifUrl
    this.cameraSvc.discoverCameraDetails(onvifUrl, onvifUserName, onvifPassword).subscribe((result: {
        cam: Camera,
        failed: Map<string, string>
      }) => {
        if (result.failed.size == 1) {
          const fKey = result.failed.keys().next().value;
          if (this.failures === undefined)
            this.failures = result.failed;
          else if (fKey !== undefined && !this.failures.has(fKey)) {
            const fVal = result.failed.values().next().value;
            if(fVal !== undefined)
              this.failures.set(fKey, fVal);
          }
        }
        if (result.cam !== undefined) {
          this.cameras.set('camera' + (this.cameras.size + 1), result.cam);
          this.fixUpCamerasData.emit();
          this.failures.delete(onvifUrl);
          // if(this.failures.size == 0)
          //     this.failures = undefined;
        } else {
          let msg = "Couldn't get camera details: - ";
          if (result.failed !== undefined && result.failed.size > 0)
            msg += result.failed.entries().next().value
          this.reporting.warningMessage = msg
        }
        this.gettingCameraDetails = false;
      },
      reason => {
        this.gettingCameraDetails = false;
        this.reporting.errorMessage = reason;
      });
  }

  setupTableFormControls() {
    this.list$ = new BehaviorSubject<[string, string][]>(Array.from(this.failures));
    const toFailureGroups = this.list$.value.map(camera => {
      return new UntypedFormGroup(
        {
          onvifUserName: new UntypedFormControl({
            value: this.onvifUserName,
            disabled: this.gettingCameraDetails
          }, [Validators.maxLength(20), Validators.minLength(0), Validators.pattern(/^[a-zA-Z0-9](_(?!([._]))|\.(?!([_.]))|[a-zA-Z0-9]){3,18}[a-zA-Z0-9]$/)]),
          onvifPassword: new UntypedFormControl({
            value: this.onvifPassword,
            disabled: this.gettingCameraDetails
          }, [Validators.maxLength(25), Validators.minLength(0), Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,64}$/)])
        });
    });

    this.failControls = new UntypedFormArray(toFailureGroups);

    // Ensure camera form controls highlight immediately if invalid
    for (let i = 0; i < this.failControls.length; ++i) {
      this.failControls.at(i).markAllAsTouched();
    }
  }

  getControl(index: number, fieldName: string): UntypedFormControl {
    return this.failControls?.at(index).get(fieldName) as UntypedFormControl;
  }
  anyInvalid(i: number) : boolean {
    return this.failControls?.at(i).invalid;
  }

  // private update(index: number, field: string, value: any) {
  //   Array.from(this.failures.values()).forEach((cam: Camera, i) => {
  //     if (i === index) { // @ts-ignore
  //       cam[field] = value;
  //     }
  //   });
  // }
  //
  // updateField(index: number, field: string) {
  //   const control = this.getControl(index, field);
  //   if (control) {
  //     this.update(index, field, control.value);
  //   }
  // }

  ngAfterViewInit(): void {
    this.setupTableFormControls();
  }

  ngOnInit(): void {
  }

  protected readonly UtilsService = UtilsService;
}
