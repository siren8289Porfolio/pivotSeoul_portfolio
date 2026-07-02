import { createBrowserRouter } from 'react-router';
import { Layout } from './components/Layout';

import { Home } from './pages/Home';
import { StageSelection } from './pages/StageSelection';
import { Onboarding } from './pages/Onboarding';
import { SimulationRun } from './pages/SimulationRun';
import { Results } from './pages/Results';

/**
 * MVP 사용자 플로우: Home → Stage → Onboarding → SimulationRun → Results
 */
export const router = createBrowserRouter([
  {
    path: '/',
    Component: Layout,
    children: [
      { index: true, Component: Home },
      { path: 'stage', Component: StageSelection },
      { path: 'onboarding', Component: Onboarding },
      { path: 'simulation-run', Component: SimulationRun },
      { path: 'results', Component: Results },
    ],
  },
]);
