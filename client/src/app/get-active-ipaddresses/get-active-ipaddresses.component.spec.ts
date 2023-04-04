import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GetActiveIPAddressesComponent } from './get-active-ipaddresses.component';

describe('GetActiveIPAddressesComponent', () => {
  let component: GetActiveIPAddressesComponent;
  let fixture: ComponentFixture<GetActiveIPAddressesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ GetActiveIPAddressesComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GetActiveIPAddressesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
