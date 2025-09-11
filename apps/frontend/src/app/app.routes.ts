import { Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { TransacoesComponent } from './components/transacoes/transacoes.component';
import { RecompensasComponent } from './components/recompensas/recompensas.component';
import { ResgatesComponent } from './components/resgates/resgates.component';
import { CampanhasComponent } from './components/campanhas/campanhas.component';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'transacoes', component: TransacoesComponent },
  { path: 'recompensas', component: RecompensasComponent },
  { path: 'resgates', component: ResgatesComponent },
  { path: 'campanhas', component: CampanhasComponent },
  { path: '**', redirectTo: '/dashboard' }
];
