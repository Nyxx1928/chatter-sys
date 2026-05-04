'use client';

/**
 * Demo page to showcase UI components
 * This file can be used for visual testing and documentation
 */

import { Button } from './Button';
import { Input, TextArea } from './Input';
import { Card, CardHeader, CardTitle, CardContent, CardFooter } from './Card';

export function UIComponentsDemo() {
  return (
    <div className="min-h-screen bg-gray-50 p-4 sm:p-8">
      <div className="max-w-4xl mx-auto space-y-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">UI Components Demo</h1>
        
        {/* Button Variants */}
        <Card>
          <CardHeader>
            <CardTitle>Button Component</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="flex flex-wrap gap-4">
                <Button variant="primary">Primary Button</Button>
                <Button variant="secondary">Secondary Button</Button>
                <Button variant="danger">Danger Button</Button>
                <Button variant="ghost">Ghost Button</Button>
              </div>
              
              <div className="flex flex-wrap gap-4">
                <Button size="sm">Small</Button>
                <Button size="md">Medium</Button>
                <Button size="lg">Large</Button>
              </div>
              
              <div>
                <Button fullWidth>Full Width Button</Button>
              </div>
              
              <div className="flex flex-wrap gap-4">
                <Button disabled>Disabled Button</Button>
              </div>
            </div>
          </CardContent>
        </Card>
        
        {/* Input Component */}
        <Card>
          <CardHeader>
            <CardTitle>Input Component</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <Input
                label="Username"
                placeholder="Enter your username"
                fullWidth
              />
              
              <Input
                label="Email"
                type="email"
                placeholder="Enter your email"
                helperText="We'll never share your email"
                fullWidth
                required
              />
              
              <Input
                label="Password"
                type="password"
                placeholder="Enter your password"
                error="Password must be at least 8 characters"
                fullWidth
              />
              
              <Input
                label="Disabled Input"
                placeholder="This is disabled"
                disabled
                fullWidth
              />
              
              <TextArea
                label="Message"
                placeholder="Enter your message"
                helperText="Maximum 500 characters"
                fullWidth
              />
            </div>
          </CardContent>
        </Card>
        
        {/* Card Variants */}
        <div className="space-y-4">
          <h2 className="text-2xl font-bold text-gray-900">Card Variants</h2>
          
          <Card variant="default">
            <CardHeader>
              <CardTitle>Default Card</CardTitle>
            </CardHeader>
            <CardContent>
              This is a default card with border styling.
            </CardContent>
            <CardFooter>
              <Button variant="primary" size="sm">Action</Button>
            </CardFooter>
          </Card>
          
          <Card variant="outlined">
            <CardHeader>
              <CardTitle>Outlined Card</CardTitle>
            </CardHeader>
            <CardContent>
              This is an outlined card with thicker border.
            </CardContent>
          </Card>
          
          <Card variant="elevated">
            <CardHeader>
              <CardTitle>Elevated Card</CardTitle>
            </CardHeader>
            <CardContent>
              This is an elevated card with shadow effects.
            </CardContent>
          </Card>
          
          <Card padding="none">
            <div className="p-4 bg-blue-50 rounded-t-lg">
              <CardTitle>Card with Custom Padding</CardTitle>
            </div>
            <div className="p-4">
              This card has no default padding, allowing for custom layouts.
            </div>
          </Card>
        </div>
        
        {/* Responsive Demo */}
        <Card variant="elevated">
          <CardHeader>
            <CardTitle>Responsive Design Demo</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="mb-4">
              All components are mobile-first and responsive. Try resizing your browser window to see how they adapt:
            </p>
            <ul className="list-disc list-inside space-y-2 text-sm">
              <li>Buttons maintain minimum 44x44px touch targets on mobile</li>
              <li>Inputs have 44px minimum height for easy tapping</li>
              <li>Cards adjust padding based on screen size (smaller on mobile)</li>
              <li>Typography scales appropriately across breakpoints</li>
            </ul>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
