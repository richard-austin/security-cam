import {Component, effect, EffectRef, ElementRef, inject, OnInit, viewChild} from '@angular/core';
import {IPDetails} from '../shared/IPDetails';
import {WifiUtilsService} from '../shared/wifi-utils.service';
import {ReportingComponent} from '../reporting/reporting.component';
import {UtilsService} from "../shared/utils.service";
import {SharedAngularMaterialModule} from "../shared/shared-angular-material/shared-angular-material.module";
import {SharedModule} from "../shared/shared.module";

@Component({
  selector: 'app-get-active-ipaddresses',
  templateUrl: './get-active-ipaddresses.component.html',
  styleUrls: ['./get-active-ipaddresses.component.scss'],
  imports: [SharedModule, SharedAngularMaterialModule]
})
export class GetActiveIPAddressesComponent implements OnInit {
  reporting = viewChild.required(ReportingComponent);
  scrollableContent = viewChild.required<ElementRef<HTMLElement>>('scrollable_content');
  private wifiUtilsService: WifiUtilsService = inject(WifiUtilsService);
  private utils: UtilsService = inject(UtilsService);

  ipDetails!: IPDetails[];
  displayedColumns: string[] = ["IP", "Name", "ConnType", "Device"];
  er: EffectRef;
  constructor() {
    this.er = effect(() => {
      const scEl = this.scrollableContent();
      const sc = scEl.nativeElement;
      sc.style = this.utils.getScrollableContentStyle(sc, true);
    });
  }

  ngOnInit(): void {
    this.wifiUtilsService.getActiveIPAddresses().subscribe({
      next: (result) => {
        this.ipDetails = result;
      },
      error: reason => {
        this.reporting().errorMessage = reason;
      }
    });
  }
}
