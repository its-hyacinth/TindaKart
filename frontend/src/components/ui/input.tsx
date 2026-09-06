import type { InputHTMLAttributes } from 'react'
import { cn } from '../../lib/utils'

export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) { return <input className={cn('min-h-12 w-full rounded-xl border border-slate-200 bg-white px-3.5 text-sm text-slate-950 shadow-sm outline-none transition focus:border-indigo-500 focus:ring-4 focus:ring-indigo-100', className)} {...props} /> }
