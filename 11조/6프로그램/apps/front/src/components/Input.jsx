import { useState } from "react";
import "./Input.css";

const Input = ({
  placeholder,
  type,
  label,
  labelClassName,
  name,
  register,
  onChange,
  eye,
  onClick,
  defaultValue,
  error,
  errorText,
  className,
  readonly,
  disabled,
  autoComplete,
}) => {
  return (
    <div className="input-wrapper">
      {label && (
        <label className={`input-label ${labelClassName || ""}`}>{label}</label>
      )}
      <div className="input-container">
        <input
          type={type}
          defaultValue={defaultValue}
          className={`input-field ${className || ""}`}
          placeholder={placeholder}
          {...(register ? register(name, { onChange }) : {})}
          readOnly={readonly}
          disabled={disabled}
          autoComplete={autoComplete}
        />
        {eye &&
          (type === "text" ? (
            <img
              alt="eye"
              className="input-eye-icon"
              onClick={() => onClick("password")}
            />
          ) : (
            <img
              alt="eye-closed"
              className="input-eye-icon"
              onClick={() => onClick("text")}
            />
          ))}
      </div>
      {error && <label className="input-error">{errorText}</label>}
    </div>
  );
};

export default Input;
