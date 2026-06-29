import React from 'react';
import { render, screen, cleanup } from '@testing-library/react';
import '@testing-library/jest-dom';
import { NavigationHeader } from '../../components/landing/NavigationHeader';
import { SYSTEM_MODULES } from '../../components/landing/navigationData';

jest.mock('next/image', () => ({
  __esModule: true,
  default: ({ fill: _fill, ...rest }: React.ImgHTMLAttributes<HTMLImageElement> & { fill?: boolean }) => {
    return <img alt="" {...rest} />;
  },
}));

jest.mock('next/link', () => ({
  __esModule: true,
  default: ({
    children,
    href,
    ...props
  }: {
    children: React.ReactNode;
    href: string;
  }) => (
    <a href={href} {...props}>
      {children}
    </a>
  ),
}));

afterEach(cleanup);

describe('NavigationHeader', () => {
  it('renders the logo and brand name', () => {
    render(<NavigationHeader />);
    const logo = screen.getByAltText('Chatter logo');
    expect(logo).toBeInTheDocument();
    expect(screen.getByText('Chatter')).toBeInTheDocument();
  });

  it('renders Platform, Community, and Stories nav items', () => {
    render(<NavigationHeader />);
    expect(screen.getAllByText('Platform').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Community').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Stories').length).toBeGreaterThanOrEqual(1);
  });

  it('renders Sign In and Get Started buttons', () => {
    render(<NavigationHeader />);
    const signInLinks = screen.getAllByText('Sign In');
    const getStartedLinks = screen.getAllByText('Get Started');
    expect(signInLinks.length).toBeGreaterThanOrEqual(1);
    expect(getStartedLinks.length).toBeGreaterThanOrEqual(1);
  });

  it('renders theme toggle button', () => {
    render(<NavigationHeader />);
    const toggle = screen.getByLabelText(/switch to (light|dark) mode/i);
    expect(toggle).toBeInTheDocument();
  });

  it('renders hamburger menu button for mobile', () => {
    render(<NavigationHeader />);
    const hamburger = screen.getByLabelText('Open menu');
    expect(hamburger).toBeInTheDocument();
  });
});

describe('NavigationHeader data', () => {
  it('exports 3 system modules', () => {
    expect(SYSTEM_MODULES).toHaveLength(3);
  });

  it('each module has a title, role, icon, and stats', () => {
    SYSTEM_MODULES.forEach((mod) => {
      expect(mod.title).toBeTruthy();
      expect(mod.role).toBeTruthy();
      expect(mod.icon).toBeTruthy();
      expect(mod.stats.length).toBeGreaterThanOrEqual(3);
    });
  });

  it('Pulse, Relay, Aurora are the three modules', () => {
    const titles = SYSTEM_MODULES.map((m) => m.title);
    expect(titles).toEqual(['Pulse', 'Relay', 'Aurora']);
  });
});
