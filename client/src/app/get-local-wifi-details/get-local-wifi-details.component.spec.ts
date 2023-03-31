import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GetLocalWifiDetailsComponent } from './get-local-wifi-details.component';

describe('GetLocalWifiDetailsComponent', () => {
  let component: GetLocalWifiDetailsComponent;
  let fixture: ComponentFixture<GetLocalWifiDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ GetLocalWifiDetailsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GetLocalWifiDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
