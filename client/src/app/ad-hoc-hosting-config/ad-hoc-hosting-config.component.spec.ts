import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdHocHostingConfigComponent } from './ad-hoc-hosting-config.component';

describe('AdHocHostingConfigComponent', () => {
  let component: AdHocHostingConfigComponent;
  let fixture: ComponentFixture<AdHocHostingConfigComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdHocHostingConfigComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdHocHostingConfigComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
